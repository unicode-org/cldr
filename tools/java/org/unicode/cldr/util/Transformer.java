package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Iterator;

import com.ibm.icu.text.Transform;

/**
 * Provides transforming iterator and iterable for convenience operations.
 *
 * @author markdavis
 *
 * @param <S>
 * @param <V>
 */
public final class Transformer<S, V> implements Iterator<V> {
    private final Iterator<? extends S> iterator;
    private final Transform<S, ? extends V> transform;
    private V nextItem;

    public static <S, V> Transformer<S, V> iterator(Transform<S, ? extends V> transform, Iterator<? extends S> iterator) {
        return new Transformer<S, V>(transform, iterator);
    }

    public static <S, V> Transformer<S, V> iterator(Transform<S, ? extends V> transform, Iterable<? extends S> iterable) {
        return new Transformer<S, V>(transform, iterable.iterator());
    }

    public static <S, V> Transformer<S, V> iterator(Transform<S, ? extends V> transform, S... items) {
        return new Transformer<S, V>(transform, Arrays.asList(items).iterator());
    }

    public static <S, V> With<V> iterable(Transform<S, ? extends V> transform, Iterator<? extends S> iterator) {
        return With.in(new Transformer<S, V>(transform, iterator));
    }

    public static <S, V> With<V> iterable(Transform<S, ? extends V> transform, Iterable<? extends S> iterable) {
        return With.in(new Transformer<S, V>(transform, iterable.iterator()));
    }

    public static <S, V> With<V> iterable(Transform<S, ? extends V> transform, S... items) {
        return With.in(new Transformer<S, V>(transform, Arrays.asList(items).iterator()));
    }

    private Transformer(Transform<S, ? extends V> transform, Iterator<? extends S> iterator) {
        this.transform = transform;
        this.iterator = iterator;
        fillInNext();
    }

    @Override
    public boolean hasNext() {
        return nextItem != null;
    }

    @Override
    public V next() {
        if (nextItem == null) {
            throw new IllegalArgumentException();
        }
        return fillInNext();
    }

    private V fillInNext() {
        V result = nextItem;
        while (iterator.hasNext()) {
            nextItem = transform.transform(iterator.next());
            if (nextItem != null) {
                return result;
            }
        }
        nextItem = null;
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
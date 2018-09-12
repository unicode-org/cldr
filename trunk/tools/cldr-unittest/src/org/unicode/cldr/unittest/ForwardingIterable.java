package org.unicode.cldr.unittest;

import java.util.Iterator;

/**
 * Small helper class that wraps an Iterator, and returns it, which is useful
 * for extended loops, which may now get initialized:
 *
 * for (Foo foo: new ForwardingIterable<Foo>(initializeFooIterator(bar))) { ...
 * }
 *
 * @author ribnitz
 *
 * @param <E>
 */
public class ForwardingIterable<E> implements Iterable<E> {

    /**
     * The iterator to forward to
     */
    private final Iterator<E> iterator;

    /**
     * Construct using the iterator provided
     *
     * @param anIterator
     */
    public ForwardingIterable(Iterator<E> anIterator) {
        iterator = anIterator;
    }

    @Override
    public Iterator<E> iterator() {
        return iterator;
    }
}
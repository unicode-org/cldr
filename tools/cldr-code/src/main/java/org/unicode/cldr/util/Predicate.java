package org.unicode.cldr.util;

public interface Predicate<T> {
    public boolean is(T item);
}
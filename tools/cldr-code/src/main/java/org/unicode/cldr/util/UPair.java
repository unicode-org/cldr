package org.unicode.cldr.util;

/**
 * A pair of objects: first and second. CLDR's Pair requires Comparables, while ICU'S doesn't have a
 * useful toString.
 *
 * @param <F> first object type
 * @param <S> second object type
 */
public class UPair<F, S> {
    public final F first;
    public final S second;

    protected UPair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Creates a pair object
     *
     * @param first must be non-null
     * @param second must be non-null
     * @return The pair object.
     */
    public static <F, S> UPair<F, S> of(F first, S second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("Pair.of requires non null values.");
        }
        return new UPair<F, S>(first, second);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof UPair)) {
            return false;
        }
        UPair<?, ?> rhs = (UPair<?, ?>) other;
        return first.equals(rhs.first) && second.equals(rhs.second);
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "[" + first + ", " + second + "]";
    }

    @Override
    public int hashCode() {
        return first.hashCode() * 37 + second.hashCode();
    }
}

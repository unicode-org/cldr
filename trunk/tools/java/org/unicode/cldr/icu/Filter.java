package org.unicode.cldr.icu;

/**
 * Interface for implementing a filter on a collection of values.
 * @author jchye
 */
public interface Filter {
    /**
     * @return true if the specified value is allowed through the filter.
     */
    public boolean includes(String value);
}

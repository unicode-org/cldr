package org.unicode.cldr.util;

import java.util.Map;

/** This is an interface for a read-only XPath. */
public interface XPathValue {
    /** How many elements are in this xpath? */
    int size();

    /** Get the nth element. Negative values are from end */
    String getElement(int i);

    /** See if the xpath contains an element */
    public boolean containsElement(String element);

    /**
     * Get the attributes for the nth element (negative index is from end). Returns null or an empty
     * map if there's nothing. PROBLEM: exposes internal map
     */
    public Map<String, String> getAttributes(int elementIndex);

    /**
     * Get the attributeValue for the attrbute at the nth element (negative index is from end).
     * Returns null if there's nothing.
     */
    String getAttributeValue(int i, String string);

    /** Does this xpath contain the attribute at all? */
    public boolean containsAttribute(String attribute);

    /** return the original string */
    public String toString();
}

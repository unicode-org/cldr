package org.unicode.cldr.util;

/** This is an interface for a read-only XPath. */
public interface XPathValue {

    /** Get the nth element. Negative values are from end */
    String getElement(int i);

    /**
     * Get the attributeValue for the attrbute at the nth element (negative index is from end).
     * Returns null if there's nothing.
     */
    String getAttributeValue(int i, String string);
}

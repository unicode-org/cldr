package org.unicode.cldr.draft;

import com.ibm.icu.text.StringTransform;

/**
 * Immutable filtered transform
 * 
 * @author markdavis
 * 
 */
public abstract class FilteredTransform implements StringTransform {
    private final StringTransform transform;

    protected FilteredTransform(StringTransform result) {
        transform = result;
    }

    public String transform(String source) {
        int last = 0;
        // TODO optimize later
        StringBuilder result = new StringBuilder();
        int[] startEnd = new int[2];
        while (getNextRegion(source, startEnd)) {
            if (startEnd[0] > last) {
                result.append(source.substring(last, startEnd[0]));
            }
            result.append(transform.transform(source.substring(startEnd[0], startEnd[1])));
            last = startEnd[1];
        }
        if (last < source.length()) {
            result.append(source.substring(last));
        }
        return result.toString();
    }

    abstract protected boolean getNextRegion(String text, int[] startEnd);

    /**
     * Subclasses will modify
     */
    public String toString() {
        return transform.toString();
    }
}

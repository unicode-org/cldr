package org.unicode.cldr.test;

/**
 * Special interface for testing; allows test cases to avoid setting up a fake CLDR file.
 */
public interface CheckAccessor {
    public String getStringValue(String path);
    public String getUnresolvedStringValue(String path);
    public String getLocaleID();
    public CheckCLDR getCause();
}

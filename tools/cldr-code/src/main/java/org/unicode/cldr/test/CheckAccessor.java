package org.unicode.cldr.test;

public interface CheckAccessor {
    public String getStringValue(String path);
    public String getUnresolvedStringValue(String path);
    public String getLocaleID();
    public CheckCLDR getCause();
}

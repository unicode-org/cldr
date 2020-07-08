package org.unicode.cldr.util;

import org.unicode.cldr.util.CLDRFile.Status;

public interface LocaleStringProvider {
    public String getStringValue(String xpath);
    public String getLocaleID();
    public String getSourceLocaleID(String xpath, Status status);
}

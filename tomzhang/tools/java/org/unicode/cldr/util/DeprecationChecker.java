package org.unicode.cldr.util;

public class DeprecationChecker {
    private XPathParts parts = new XPathParts();
    private SupplementalDataInfo supplementalDataInfo;

    public DeprecationChecker() {
        this.supplementalDataInfo = SupplementalDataInfo.getInstance();
    }

    public DeprecationChecker(SupplementalDataInfo supplementalDataInfo) {
        this.supplementalDataInfo = supplementalDataInfo;
    }

    public boolean isBoilerplate(String path) {
        return path.endsWith("/alias")
            || path.endsWith("/default")
            || path.startsWith("//ldml/identity")
            || path.startsWith("//ldml/numbers/symbols/")
            || path.startsWith("//ldml/numbers/currencyFormats/");
    }

    public boolean isDeprecated(String path) {
        synchronized (this) {
            parts.set(path);
            return supplementalDataInfo.hasDeprecatedItem(parts.getElement(0), parts);
        }
    }
}

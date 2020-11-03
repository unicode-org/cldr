package org.unicode.cldr.util;

/**
 * Simple implementation around a base URL
 * @author srl
 *
 */
public class StaticCLDRURLS extends CLDRURLS {
    private final String base;

    public StaticCLDRURLS(String base) {
        this.base = base;
    }

    @Override
    public String base() {
        return base;
    }
}

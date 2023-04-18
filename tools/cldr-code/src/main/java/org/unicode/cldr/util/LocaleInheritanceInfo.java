package org.unicode.cldr.util;

/**
 * A triple with information about why an inheritance worked the way it did
 */
public final class LocaleInheritanceInfo {
    /**
     * Reason this entry is there
     */
    enum Reason {
        value("A value was present at this location"),
        codefallback("This value represents an implicit value per spec"),
        itemalias("An alias was found at this location"),
        none("No value was found"),
        constructed("This xpath contributes to a constructed (fallback) value"),
        novalue("The value was not found in this locale."),
        inheritancemarker("An inheritance marker ↑↑↑ was found here"),
        alt("An alt attribute was removed"),
        count("A count attribute was changed"),
        ;

        private String description;

        Reason(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return this.name() + ": " + description;
        }
    }

    private String locale;
    public String getLocale() {
        return locale;
    }

    private String path;
    public String getPath() {
        return path;
    }

    private Reason reason;

    public Reason getReason() {
        return reason;
    }

    /**
     * @param locale required locale
     * @param path optional xpath
     * @param reason required reason
     */
    LocaleInheritanceInfo(String locale, String path, Reason reason) {
        this.locale = locale;
        this.path = path;
        this.reason = reason;
    }

    @Override
    public
    String toString() {
        if (locale == null && path == null) {
            return reason.name();
        } else if (path == null) {
            return String.format("%s: locale %s", reason.name(), locale);
        } else if (locale == null) {
            return String.format("%s: %s", reason.name(), path);
        } else {
            return String.format("%s: %s:%s", reason.name(), locale, path);
        }
    }

    @Override
    public
    boolean equals(Object other) {
        if (!(other instanceof LocaleInheritanceInfo)) return false;
        final LocaleInheritanceInfo o = (LocaleInheritanceInfo)other;
        if (o.reason != reason) return false;
        if (!equals(locale, o.locale)) return false;
        if (!equals(path, o.path)) return false;
        return true;
    }

    private static final boolean equals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null && b != null) return false;
        return a.equals(b);
    }
}

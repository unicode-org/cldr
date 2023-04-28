package org.unicode.cldr.util;

/** A triple with information about why an inheritance worked the way it did */
public final class LocaleInheritanceInfo {
    /** Reason this entry is there */
    public enum Reason {
        value("Found: explicit value"),
        codeFallback("Found: code fallback"),
        alias("An alias was found at this location"),
        constructed("Constructed value"),
        none("The value was not found in this locale."),
        inheritanceMarker("Found: Inheritance marker"),
        removedAttribute("Removed attribute: ${attribute}"), // such as alt
        changedAttribute("Changed attribute: ${attribute}"), // such as count
        ;

        private String description;

        Reason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }

        @Override
        public String toString() {
            return this.name() + ": " + description;
        }
    }

    private String locale;

    /**
     * Optional locale for this entry. or null
     *
     * @return
     */
    public String getLocale() {
        return locale;
    }

    private String path;

    /**
     * Optional path for this entry, or null
     *
     * @return
     */
    public String getPath() {
        return path;
    }

    private Reason reason;

    /**
     * Reason enum for this entry
     *
     * @return
     */
    public Reason getReason() {
        return reason;
    }

    private String attribute = null;

    /**
     * Which attribute was involved (for Reason.removedAttribute/Reason.changedAttribute)
     *
     * @return
     */
    public String getAttribute() {
        return attribute;
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

    LocaleInheritanceInfo(String locale, String path, Reason reason, String attribute) {
        this.locale = locale;
        this.path = path;
        this.reason = reason;
        this.attribute = attribute;
    }

    @Override
    public String toString() {
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
    public boolean equals(Object other) {
        if (!(other instanceof LocaleInheritanceInfo)) return false;
        final LocaleInheritanceInfo o = (LocaleInheritanceInfo) other;
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

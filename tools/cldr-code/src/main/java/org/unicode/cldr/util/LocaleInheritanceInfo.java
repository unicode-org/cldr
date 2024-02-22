package org.unicode.cldr.util;

/** A class with information about why an inheritance worked the way it did */
public final class LocaleInheritanceInfo {
    /** Reason this entry is there */
    public enum Reason {
        /**
         * An actual value was found in the XML source. This is never returned for constructed or
         * other synthesized values.
         */
        value("Found: explicit value", true),
        /**
         * codeFallback does not have a locale, it is effectively the parent of 'root'. A value was
         * calculated according to the specification.
         */
        codeFallback("Found: code fallback", true),
        /**
         * This shows the location where an alias was found which affected the inheritance chain.
         */
        alias("An alias was found at this location", false),
        /**
         * Constructed entries form a block. All such values are in place of the actual constructed
         * value.
         */
        constructed("Constructed value", false),
        none("The value was not found in this locale.", true),
        inheritanceMarker("Found: Inheritance marker", false),
        removedAttribute("Removed attribute: ${attribute}", false),
        changedAttribute("Changed attribute: ${attribute}", false),
        /**
         * @see CLDRFile#getFallbackPath - used for other fallback paths
         */
        fallback("Other fallback path", false);

        private String description;
        private boolean terminal;

        /**
         * An entry is 'terminal' if it represents the end of a successful look up chain. A
         * nonterminal entry merely contributes to the look up. Only terminal entries will
         * correspond to a return value from {@link CLDRFile#getSourceLocaleIdExtended(String,
         * org.unicode.cldr.util.CLDRFile.Status, boolean)} for example. Entries following a
         * terminal entry are the start of a bailey value.
         *
         * @return
         */
        public boolean isTerminal() {
            return terminal;
        }

        Reason(String description, boolean terminal) {
            this.description = description;
            this.terminal = terminal;
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

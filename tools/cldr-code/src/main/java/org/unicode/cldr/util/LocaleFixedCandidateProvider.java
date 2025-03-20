package org.unicode.cldr.util;

/** A locale-based FixedCandidateProvider. */
public abstract class LocaleFixedCandidateProvider extends FixedCandidateProvider {
    private final CLDRLocale locale;

    LocaleFixedCandidateProvider(final CLDRLocale locale) {
        this.locale = locale;
    }

    public CLDRLocale getLocale() {
        return locale;
    }

    public SupplementalDataInfo getSupplementalDataInfo() {
        return SupplementalDataInfo.getInstance();
    }
}

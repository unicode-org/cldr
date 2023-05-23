package org.unicode.cldr.util;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.TreeSet;

public class LocaleSet {

    private final Set<CLDRLocale> set = new TreeSet<>();
    private boolean isAllLocales = false;

    public LocaleSet() {}

    public LocaleSet(boolean isAllLocales) {
        this.isAllLocales = isAllLocales;
    }

    public LocaleSet(Set<String> localeNameSet) {
        for (String s : localeNameSet) {
            set.add(CLDRLocale.getInstance(s));
        }
    }

    public void add(CLDRLocale locale) {
        set.add(locale);
    }

    public void addAll(Set<CLDRLocale> localeListSet) {
        set.addAll(localeListSet);
    }

    public boolean contains(CLDRLocale locale) {
        return isAllLocales || set.contains(locale);
    }

    public boolean containsLocaleOrParent(CLDRLocale locale) {
        if (isAllLocales || set.contains(locale)) {
            return true;
        }
        final CLDRLocale parent = locale.getParent();
        return parent != null && set.contains(parent);
    }

    @Override
    public String toString() {
        if (isAllLocales) {
            return LocaleNormalizer.ALL_LOCALES;
        }
        final Set<String> strSet = new TreeSet<>();
        for (CLDRLocale loc : set) {
            strSet.add(loc.getBaseName());
        }
        return String.join(" ", strSet);
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public boolean isAllLocales() {
        return isAllLocales;
    }

    public CLDRLocale firstElement() {
        return set.iterator().next();
    }

    public Set<CLDRLocale> getSet() {
        if (isAllLocales) {
            throw new IllegalArgumentException("Do not call getSet if isAllLocales");
        }
        return set;
    }

    public boolean intersectionNonEmpty(LocaleSet otherSet) {
        if (isEmpty() || otherSet.isEmpty()) {
            return false;
        }
        if (isAllLocales || otherSet.isAllLocales) {
            return true;
        }
        return !Sets.intersection(getSet(), otherSet.getSet()).isEmpty();
    }

    /**
     * Given a set that may include regional variants (sublocales) for the same language, return a
     * set in which such sublocales have been combined under the main language name. For example,
     * given "aa fr_BE fr_CA zh", return "aa fr zh"
     *
     * <p>This method is intended to combined regional variants in the same way they are combined
     * for SurveyForum.java.
     *
     * @return the set in which regional variants have been combined
     */
    public LocaleSet combineRegionalVariants() {
        if (isAllLocales || isEmpty()) {
            return this;
        }
        Set<String> languageSet = new TreeSet<>();
        for (CLDRLocale locale : getSet()) {
            languageSet.add(locale.getLanguage());
        }
        return new LocaleSet(languageSet);
    }
}
